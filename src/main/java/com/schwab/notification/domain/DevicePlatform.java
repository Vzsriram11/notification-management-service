package com.schwab.notification.domain;

/**
 * Which push platform a recipient's device is registered on. PushChannelProvider
 * uses this to pick APNs vs FCM — the brownfield "refactor provider-specific
 * logic" scenario, applied to the PUSH channel.
 */
public enum DevicePlatform {
    IOS, ANDROID
}
