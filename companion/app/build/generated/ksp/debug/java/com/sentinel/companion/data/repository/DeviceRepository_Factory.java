package com.sentinel.companion.data.repository;

import com.sentinel.companion.data.db.DeviceDao;
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
public final class DeviceRepository_Factory implements Factory<DeviceRepository> {
  private final Provider<DeviceDao> daoProvider;

  public DeviceRepository_Factory(Provider<DeviceDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public DeviceRepository get() {
    return newInstance(daoProvider.get());
  }

  public static DeviceRepository_Factory create(Provider<DeviceDao> daoProvider) {
    return new DeviceRepository_Factory(daoProvider);
  }

  public static DeviceRepository newInstance(DeviceDao dao) {
    return new DeviceRepository(dao);
  }
}
