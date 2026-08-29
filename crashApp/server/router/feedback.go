package router

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
)

const giteeIssuesURL = "https://gitee.com/api/v5/repos/kagg886/sylu-educational-office-accesser/issues"

type feedbackRequest struct {
	Content string `json:"content"`
	Token   string `json:"token"`
	System  string `json:"system"`
	Version string `json:"version"`
}

type giteeIssueRequest struct {
	Title string `json:"title"`
	Body  string `json:"body"`
}

type giteeIssueResponse struct {
	HTMLURL string `json:"html_url"`
}

func (h *handlers) createFeedback(context *gin.Context) {
	var request feedbackRequest
	if err := context.ShouldBindJSON(&request); err != nil {
		fail(context, http.StatusBadRequest, errors.New("invalid JSON payload"))
		return
	}
	if strings.TrimSpace(request.Content) == "" {
		fail(context, http.StatusBadRequest, errors.New("content is required"))
		return
	}
	if strings.TrimSpace(request.Token) == "" {
		fail(context, http.StatusBadRequest, errors.New("token is required"))
		return
	}
	_, deviceID, ok := h.dependencies.Tokens.Take(request.Token)
	if !ok {
		fail(context, http.StatusBadRequest, errors.New("token is invalid, expired, or already used"))
		return
	}
	if strings.TrimSpace(h.dependencies.GiteeToken) == "" {
		fail(context, http.StatusInternalServerError, errors.New("Gitee token is not configured"))
		return
	}

	body := fmt.Sprintf(
		"# 账户匿名ID `%s`\n# 系统版本\n%s\n# 运行平台\n%s\n# 反馈内容\n%s",
		deviceID,
		request.Version,
		request.System,
		request.Content,
	)
	payload, err := json.Marshal(giteeIssueRequest{
		Title: "SYLU - EOA 软件反馈",
		Body:  body,
	})
	if err != nil {
		fail(context, http.StatusInternalServerError, errors.New("failed to encode Gitee issue"))
		return
	}

	requestToGitee, err := http.NewRequestWithContext(
		context.Request.Context(),
		http.MethodPost,
		giteeIssuesURL,
		bytes.NewReader(payload),
	)
	if err != nil {
		fail(context, http.StatusInternalServerError, errors.New("failed to create Gitee request"))
		return
	}
	requestToGitee.Header.Set("Accept", "application/json")
	requestToGitee.Header.Set("Content-Type", "application/json")
	query := requestToGitee.URL.Query()
	query.Set("access_token", h.dependencies.GiteeToken)
	requestToGitee.URL.RawQuery = query.Encode()

	client := &http.Client{Timeout: 15 * time.Second}
	response, err := client.Do(requestToGitee)
	if err != nil {
		fail(context, http.StatusBadGateway, errors.New("failed to create Gitee issue"))
		return
	}
	defer response.Body.Close()

	responseBody, err := io.ReadAll(io.LimitReader(response.Body, 1<<20))
	if err != nil {
		fail(context, http.StatusBadGateway, errors.New("failed to read Gitee response"))
		return
	}
	if response.StatusCode < http.StatusOK || response.StatusCode >= http.StatusMultipleChoices {
		fail(context, http.StatusBadGateway, fmt.Errorf("Gitee returned status %s", response.Status))
		return
	}

	var issue giteeIssueResponse
	if err := json.Unmarshal(responseBody, &issue); err != nil || strings.TrimSpace(issue.HTMLURL) == "" {
		fail(context, http.StatusBadGateway, errors.New("Gitee returned an invalid issue response"))
		return
	}
	succeed(context, issue.HTMLURL)
}
