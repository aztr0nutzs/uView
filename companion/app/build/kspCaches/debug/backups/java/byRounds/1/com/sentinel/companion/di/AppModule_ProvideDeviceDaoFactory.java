package com.sentinel.companion.di;

import com.sentinel.companion.data.db.AppDatabase;
import com.sentinel.companion.data.db.DeviceDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideDeviceDaoFactory implements Factory<DeviceDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideDeviceDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public DeviceDao get() {
    return provideDeviceDao(dbProvider.get());
  }

  public static AppModule_ProvideDeviceDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideDeviceDaoFactory(dbProvider);
  }

  public static DeviceDao provideDeviceDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideDeviceDao(db));
  }
}
