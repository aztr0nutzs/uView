package com.sentinel.companion.data.repository;

import com.sentinel.companion.data.db.AlertDao;
import com.sentinel.companion.data.db.CameraDao;
import com.sentinel.companion.data.sync.CompanionSyncService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class CameraRepository_Factory implements Factory<CameraRepository> {
  private final Provider<CameraDao> cameraDaoProvider;

  private final Provider<AlertDao> alertDaoProvider;

  private final Provider<PreferencesRepository> prefsRepoProvider;

  private final Provider<CompanionSyncService> syncServiceProvider;

  public CameraRepository_Factory(Provider<CameraDao> cameraDaoProvider,
      Provider<AlertDao> alertDaoProvider, Provider<PreferencesRepository> prefsRepoProvider,
      Provider<CompanionSyncService> syncServiceProvider) {
    this.cameraDaoProvider = cameraDaoProvider;
    this.alertDaoProvider = alertDaoProvider;
    this.prefsRepoProvider = prefsRepoProvider;
    this.syncServiceProvider = syncServiceProvider;
  }

  @Override
  public CameraRepository get() {
    return newInstance(cameraDaoProvider.get(), alertDaoProvider.get(), prefsRepoProvider.get(), syncServiceProvider.get());
  }

  public static CameraRepository_Factory create(Provider<CameraDao> cameraDaoProvider,
      Provider<AlertDao> alertDaoProvider, Provider<PreferencesRepository> prefsRepoProvider,
      Provider<CompanionSyncService> syncServiceProvider) {
    return new CameraRepository_Factory(cameraDaoProvider, alertDaoProvider, prefsRepoProvider, syncServiceProvider);
  }

  public static CameraRepository newInstance(CameraDao cameraDao, AlertDao alertDao,
      PreferencesRepository prefsRepo, CompanionSyncService syncService) {
    return new CameraRepository(cameraDao, alertDao, prefsRepo, syncService);
  }
}
