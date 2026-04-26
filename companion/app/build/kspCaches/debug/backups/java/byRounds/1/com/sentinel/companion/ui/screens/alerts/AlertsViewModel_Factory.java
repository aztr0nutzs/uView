package com.sentinel.companion.ui.screens.alerts;

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
public final class AlertsViewModel_Factory implements Factory<AlertsViewModel> {
  private final Provider<CameraRepository> repoProvider;

  public AlertsViewModel_Factory(Provider<CameraRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public AlertsViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static AlertsViewModel_Factory create(Provider<CameraRepository> repoProvider) {
    return new AlertsViewModel_Factory(repoProvider);
  }

  public static AlertsViewModel newInstance(CameraRepository repo) {
    return new AlertsViewModel(repo);
  }
}
