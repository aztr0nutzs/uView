package com.sentinel.companion.di;

import com.sentinel.companion.data.db.DeviceDao;
import com.sentinel.companion.data.repository.DeviceRepository;
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
public final class AppModule_ProvideDeviceRepositoryFactory implements Factory<DeviceRepository> {
  private final Provider<DeviceDao> daoProvider;

  public AppModule_ProvideDeviceRepositoryFactory(Provider<DeviceDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public DeviceRepository get() {
    return provideDeviceRepository(daoProvider.get());
  }

  public static AppModule_ProvideDeviceRepositoryFactory create(Provider<DeviceDao> daoProvider) {
    return new AppModule_ProvideDeviceRepositoryFactory(daoProvider);
  }

  public static DeviceRepository provideDeviceRepository(DeviceDao dao) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideDeviceRepository(dao));
  }
}
