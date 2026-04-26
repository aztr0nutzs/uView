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
public final class StreamEndpointTester_Factory implements Factory<StreamEndpointTester> {
  @Override
  public StreamEndpointTester get() {
    return newInstance();
  }

  public static StreamEndpointTester_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static StreamEndpointTester newInstance() {
    return new StreamEndpointTester();
  }

  private static final class InstanceHolder {
    private static final StreamEndpointTester_Factory INSTANCE = new StreamEndpointTester_Factory();
  }
}
