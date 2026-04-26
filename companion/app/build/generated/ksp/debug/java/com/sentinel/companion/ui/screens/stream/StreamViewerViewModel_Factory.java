package com.sentinel.companion.ui.screens.stream;

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
public final class StreamViewerViewModel_Factory implements Factory<StreamViewerViewModel> {
  private final Provider<DeviceRepository> repoProvider;

  public StreamViewerViewModel_Factory(Provider<DeviceRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public StreamViewerViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static StreamViewerViewModel_Factory create(Provider<DeviceRepository> repoProvider) {
    return new StreamViewerViewModel_Factory(repoProvider);
  }

  public static StreamViewerViewModel newInstance(DeviceRepository repo) {
    return new StreamViewerViewModel(repo);
  }
}
