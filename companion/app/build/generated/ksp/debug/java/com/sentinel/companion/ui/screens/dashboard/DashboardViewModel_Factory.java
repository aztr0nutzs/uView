package com.sentinel.companion.ui.screens.dashboard;

import com.sentinel.companion.data.repository.CameraRepository;
import com.sentinel.companion.data.repository.DeviceRepository;
import com.sentinel.companion.data.repository.PreferencesRepository;
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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<CameraRepository> cameraRepoProvider;

  private final Provider<DeviceRepository> deviceRepoProvider;

  private final Provider<PreferencesRepository> prefsRepoProvider;

  public DashboardViewModel_Factory(Provider<CameraRepository> cameraRepoProvider,
      Provider<DeviceRepository> deviceRepoProvider,
      Provider<PreferencesRepository> prefsRepoProvider) {
    this.cameraRepoProvider = cameraRepoProvider;
    this.deviceRepoProvider = deviceRepoProvider;
    this.prefsRepoProvider = prefsRepoProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(cameraRepoProvider.get(), deviceRepoProvider.get(), prefsRepoProvider.get());
  }

  public static DashboardViewModel_Factory create(Provider<CameraRepository> cameraRepoProvider,
      Provider<DeviceRepository> deviceRepoProvider,
      Provider<PreferencesRepository> prefsRepoProvider) {
    return new DashboardViewModel_Factory(cameraRepoProvider, deviceRepoProvider, prefsRepoProvider);
  }

  public static DashboardViewModel newInstance(CameraRepository cameraRepo,
      DeviceRepository deviceRepo, PreferencesRepository prefsRepo) {
    return new DashboardViewModel(cameraRepo, deviceRepo, prefsRepo);
  }
}
