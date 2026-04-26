package com.sentinel.companion.ui.screens.devicesettings;

import com.sentinel.companion.data.repository.DeviceRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class DeviceSettingsViewModel_Factory implements Factory<DeviceSettingsViewModel> {
  private final Provider<DeviceRepository> repoProvider;

  public DeviceSettingsViewModel_Factory(Provider<DeviceRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public DeviceSettingsViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static DeviceSettingsViewModel_Factory create(Provider<DeviceRepository> repoProvider) {
    return new DeviceSettingsViewModel_Factory(repoProvider);
  }

  public static DeviceSettingsViewModel newInstance(DeviceRepository repo) {
    return new DeviceSettingsViewModel(repo);
  }
}
