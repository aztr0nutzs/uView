package com.sentinel.companion.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
  @Override
  public CameraRepository get() {
    return newInstance();
  }

  public static CameraRepository_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CameraRepository newInstance() {
    return new CameraRepository();
  }

  private static final class InstanceHolder {
    private static final CameraRepository_Factory INSTANCE = new CameraRepository_Factory();
  }
}
