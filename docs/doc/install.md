# 安装

EOA 目前提供 Android 和 iOS 两种安装方式。Android 可以直接安装，iOS 需要多走几步。

::: tip 版本支持
本应用支持 Android 6 及以上版本，以及 iOS 16.2 及以上版本。

Android 需要按照自己的系统版本选择不同的安装包；iOS 只需要下载 `ios.ipa`，再按照[下面的教程](#ios)安装。
:::

::: danger 旧系统无法使用
Android 5 及以下、iOS 16.1 及以下无法正常安装或使用本应用。遇到这种情况，只能升级系统或换一台系统版本更高的设备。
:::

## Android

Android 可以直接下载安装包。安装前先确认自己的系统版本，再选择对应的文件。

1. 先看一下手机的 Android 版本。

::: tip 如何查看 Android 版本
一般可以在手机的“设置”里找到：

1. 打开“设置”。
2. 找到“关于手机”或“我的设备”。
3. 查看“Android 版本”。

不同品牌的入口名字可能不完全一样。如果找不到，可以在设置顶部的搜索框里搜索“Android 版本”。
:::

2. Android 9 及以上，下载 [`app-release.apk`](https://gitee.com/kagg886/sylu-educational-office-accesser/releases/download/latest/app-release.apk)。
3. Android 6 到 Android 8，下载 [`app-release-6.apk`](https://gitee.com/kagg886/sylu-educational-office-accesser/releases/download/latest/app-release-6.apk)。
4. 下载完成后，点击这个文件开始安装。

::: warning 安装风险提示
国产手机系统经常会对不是应用商店下载的安装包弹出“有风险”“未知来源”或类似提醒。只要安装包来自本项目的发布页面，就可以选择继续安装。
:::

::: warning 黑边问题
如果打开软件后发现底部有一大块黑边，说明你可能在 Android 6 到 Android 8 的手机上安装了 Android 9 及以上使用的安装包。

遇到这种情况，重新安装 [`app-release-6.apk`](https://gitee.com/kagg886/sylu-educational-office-accesser/releases/download/latest/app-release-6.apk) 这个特供版本即可。
:::

## iOS

安装前可以先确认一下 iPhone 的系统版本。本应用需要 iOS 16.2 或更高版本。

::: tip 如何查看 iOS 版本
打开“设置”，进入“通用”，再进入“关于本机”，查看“iOS 版本”。
:::

1. 下载 [`ios.ipa`](https://gitee.com/kagg886/sylu-educational-office-accesser/releases/download/latest/ios.ipa)。
2. 打开这个教程：<https://livecontainer.github.io/zh-CN/docs/installation/lc_sidestore#%E6%96%B9%E6%B3%95-2iloader>
3. 按照教程里的“方法 2：iLoader”步骤，把 `ios.ipa` 安装到手机上。

::: tip 提示
iOS 目前需要通过侧载方式安装。苹果开发者账号年费为 688 RMB，之后会考虑上架 App Store。上架后，大家就可以直接在 App Store 搜索本应用并下载安装。
:::
