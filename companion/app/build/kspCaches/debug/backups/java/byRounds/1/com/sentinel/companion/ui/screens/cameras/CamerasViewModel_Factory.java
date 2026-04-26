package com.sentinel.companion.ui.screens.cameras;

import com.sentinel.companion.data.repository.CameraRepository;
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
public final class CamerasViewModel_Factory implements Factory<CamerasViewModel> {
  private final Provider<CameraRepository> repoProvider;

  public CamerasViewModel_Factory(Provider<CameraRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public CamerasViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static CamerasViewModel_Factory create(Provider<CameraRepository> repoProvider) {
    return new CamerasViewModel_Factory(repoProvider);
  }

  public static CamerasViewModel newInstance(CameraRepository repo) {
    return new CamerasViewModel(repo);
  }
}
