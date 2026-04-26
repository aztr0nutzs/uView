package com.sentinel.companion.data.network;

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
public final class HostValidator_Factory implements Factory<HostValidator> {
  @Override
  public HostValidator get() {
    return newInstance();
  }

  public static HostValidator_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static HostValidator newInstance() {
    return new HostValidator();
  }

  private static final class InstanceHolder {
    private static final HostValidator_Factory INSTANCE = new HostValidator_Factory();
  }
}
