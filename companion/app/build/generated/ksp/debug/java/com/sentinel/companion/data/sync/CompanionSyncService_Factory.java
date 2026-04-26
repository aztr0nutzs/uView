package com.sentinel.companion.data.sync;

import com.sentinel.companion.data.db.AlertDao;
import com.sentinel.companion.data.db.CameraDao;
import com.sentinel.companion.data.repository.PreferencesRepository;
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
public final class CompanionSyncService_Factory implements Factory<CompanionSyncService> {
  private final Provider<CameraDao> cameraDaoProvider;

  private final Provider<AlertDao> alertDaoProvider;

  private final Provider<PreferencesRepository> prefsRepoProvider;

  public CompanionSyncService_Factory(Provider<CameraDao> cameraDaoProvider,
      Provider<AlertDao> alertDaoProvider, Provider<PreferencesRepository> prefsRepoProvider) {
    this.cameraDaoProvider = cameraDaoProvider;
    this.alertDaoProvider = alertDaoProvider;
    this.prefsRepoProvider = prefsRepoProvider;
  }

  @Override
  public CompanionSyncService get() {
    return newInstance(cameraDaoProvider.get(), alertDaoProvider.get(), prefsRepoProvider.get());
  }

  public static CompanionSyncService_Factory create(Provider<CameraDao> cameraDaoProvider,
      Provider<AlertDao> alertDaoProvider, Provider<PreferencesRepository> prefsRepoProvider) {
    return new CompanionSyncService_Factory(cameraDaoProvider, alertDaoProvider, prefsRepoProvider);
  }

  public static CompanionSyncService newInstance(CameraDao cameraDao, AlertDao alertDao,
      PreferencesRepository prefsRepo) {
    return new CompanionSyncService(cameraDao, alertDao, prefsRepo);
  }
}
