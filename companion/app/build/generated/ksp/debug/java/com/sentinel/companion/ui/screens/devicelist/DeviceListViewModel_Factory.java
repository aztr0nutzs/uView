package com.sentinel.companion.ui.screens.devicelist;

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
public final class DeviceListViewModel_Factory implements Factory<DeviceListViewModel> {
  private final Provider<DeviceRepository> repoProvider;

  public DeviceListViewModel_Factory(Provider<DeviceRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public DeviceListViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static DeviceListViewModel_Factory create(Provider<DeviceRepository> repoProvider) {
    return new DeviceListViewModel_Factory(repoProvider);
  }

  public static DeviceListViewModel newInstance(DeviceRepository repo) {
    return new DeviceListViewModel(repo);
  }
}
