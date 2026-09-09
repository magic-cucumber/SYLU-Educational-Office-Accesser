package router

import (
	"errors"
	"fmt"
	"io"
	"log"
	"mime/multipart"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"strings"

	"github.com/gin-gonic/gin"
	"server/util"
)

const multipartOverheadAllowance int64 = 64 * 1024

var reportFilenamePattern = regexp.MustCompile(`(?i)^(?:[0-9a-f]{32}|[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12})\.bin$`)

type reportTokenRequest struct {
	Cipher   string `json:"cipher"`
	DeviceID string `json:"deviceId"`
}

func (h *handlers) createReportToken(context *gin.Context) {
	var request reportTokenRequest
	if err := context.ShouldBindJSON(&request); err != nil {
		fail(context, http.StatusBadRequest, errors.New("invalid JSON payload"))
		return
	}
	deviceID := strings.TrimSpace(request.DeviceID)
	if deviceID == "" {
		fail(context, http.StatusBadRequest, errors.New("deviceId is required"))
		return
	}
	if reason, ok := h.dependencies.Blacklist[deviceID]; ok {
		fail(context, http.StatusOK, errors.New(reason))
		return
	}
	var keyError error
	token, err := h.dependencies.Reports.GetOrCreateToken(deviceID, func() ([]byte, error) {
		key, err := util.DecryptRSACipher(h.dependencies.PrivateKey, strings.TrimSpace(request.Cipher), 32)
		keyError = err
		return key, err
	})
	if err != nil {
		if keyError != nil {
			fail(context, http.StatusBadRequest, keyError)
			return
		}
		fail(context, http.StatusInternalServerError, errors.New("failed to generate upload token"))
		return
	}
	succeed(context, token)
}

func (h *handlers) uploadReport(context *gin.Context) {
	limit := h.dependencies.MaxTransportSize
	if limit <= (1<<63-1)-multipartOverheadAllowance {
		limit += multipartOverheadAllowance
	}
	context.Request.Body = http.MaxBytesReader(context.Writer, context.Request.Body, limit)
	if err := context.Request.ParseMultipartForm(32 << 10); err != nil {
		status := http.StatusBadRequest
		message := errors.New("invalid multipart upload")
		var maxBytesError *http.MaxBytesError
		if errors.As(err, &maxBytesError) {
			status = http.StatusRequestEntityTooLarge
			message = errors.New("multipart upload exceeds --max-transport-size")
		}
		fail(context, status, message)
		return
	}
	defer context.Request.MultipartForm.RemoveAll()

	token := context.Request.FormValue("token")
	if token == "" {
		fail(context, http.StatusBadRequest, errors.New("token is required"))
		return
	}
	claim, ok := h.dependencies.Reports.BeginUpload(token)
	if !ok {
		fail(context, http.StatusBadRequest, errors.New("token is invalid, expired, or already used"))
		return
	}
	if claim.Role == util.UploadFollower {
		result, completed := claim.Task.Wait(context.Request.Context())
		if completed {
			writeResult(context, result)
		}
		return
	}

	result := h.processOwnedUpload(context, claim)
	writeResult(context, result)
}

func (h *handlers) processOwnedUpload(context *gin.Context, claim util.UploadClaim) (result util.UploadResult) {
	result = failureResult(http.StatusInternalServerError, errors.New("failed to process report upload"))
	defer func() {
		if recovered := recover(); recovered != nil {
			log.Printf("panic while processing report upload: %v", recovered)
		}
		claim.Task.Complete(result)
	}()
	return h.processUpload(context, claim.Task.AESKey())
}

func (h *handlers) processUpload(context *gin.Context, aesKey []byte) util.UploadResult {

	fileHeader, err := firstFile(context.Request.MultipartForm, "file")
	if err != nil {
		return failureResult(http.StatusBadRequest, err)
	}
	if !reportFilenamePattern.MatchString(fileHeader.Filename) {
		return failureResult(http.StatusBadRequest, errors.New("file name must be a UUID with .bin extension"))
	}
	if fileHeader.Header.Get("Content-Type") != "application/octet-stream" {
		return failureResult(http.StatusBadRequest, errors.New("file content type must be application/octet-stream"))
	}
	if fileHeader.Size > h.dependencies.MaxTransportSize {
		return failureResult(http.StatusRequestEntityTooLarge, errors.New("encrypted report exceeds --max-transport-size"))
	}

	file, err := fileHeader.Open()
	if err != nil {
		return failureResult(http.StatusBadRequest, errors.New("failed to open uploaded file"))
	}
	defer file.Close()
	encrypted, err := io.ReadAll(io.LimitReader(file, h.dependencies.MaxTransportSize+1))
	if err != nil {
		return failureResult(http.StatusBadRequest, errors.New("failed to read uploaded file"))
	}
	if int64(len(encrypted)) > h.dependencies.MaxTransportSize {
		return failureResult(http.StatusRequestEntityTooLarge, errors.New("encrypted report exceeds --max-transport-size"))
	}
	plaintext, err := util.DecryptReport(aesKey, encrypted)
	if err != nil {
		return failureResult(http.StatusBadRequest, err)
	}

	name := strings.TrimSuffix(fileHeader.Filename, filepath.Ext(fileHeader.Filename)) + ".zip"
	if err := writeExclusive(filepath.Join(h.dependencies.SaveDir, name), plaintext); err != nil {
		return failureResult(http.StatusInternalServerError, err)
	}
	return successResult(nil)
}

func firstFile(form *multipart.Form, field string) (*multipart.FileHeader, error) {
	if form == nil || len(form.File[field]) != 1 {
		return nil, fmt.Errorf("exactly one %s file is required", field)
	}
	return form.File[field][0], nil
}

func writeExclusive(path string, contents []byte) error {
	file, err := os.OpenFile(path, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0o640)
	if err != nil {
		if errors.Is(err, os.ErrExist) {
			return errors.New("a report with the same UUID already exists")
		}
		return errors.New("failed to create decrypted ZIP")
	}
	succeeded := false
	defer func() {
		_ = file.Close()
		if !succeeded {
			_ = os.Remove(path)
		}
	}()
	if _, err := file.Write(contents); err != nil {
		return errors.New("failed to write decrypted ZIP")
	}
	if err := file.Sync(); err != nil {
		return errors.New("failed to flush decrypted ZIP")
	}
	if err := file.Close(); err != nil {
		return errors.New("failed to close decrypted ZIP")
	}
	succeeded = true
	return nil
}
