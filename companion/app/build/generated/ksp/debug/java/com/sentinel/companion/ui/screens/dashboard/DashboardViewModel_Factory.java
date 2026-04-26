package com.sentinel.companion.ui.screens.dashboard;

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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<CameraRepository> repoProvider;

  public DashboardViewModel_Factory(Provider<CameraRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static DashboardViewModel_Factory create(Provider<CameraRepository> repoProvider) {
    return new DashboardViewModel_Factory(repoProvider);
  }

  public static DashboardViewModel newInstance(CameraRepository repo) {
    return new DashboardViewModel(repo);
  }
}
