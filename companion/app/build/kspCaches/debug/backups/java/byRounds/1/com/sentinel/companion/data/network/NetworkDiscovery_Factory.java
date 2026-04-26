package com.sentinel.companion.data.network;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class NetworkDiscovery_Factory implements Factory<NetworkDiscovery> {
  private final Provider<Context> contextProvider;

  public NetworkDiscovery_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public NetworkDiscovery get() {
    return newInstance(contextProvider.get());
  }

  public static NetworkDiscovery_Factory create(Provider<Context> contextProvider) {
    return new NetworkDiscovery_Factory(contextProvider);
  }

  public static NetworkDiscovery newInstance(Context context) {
    return new NetworkDiscovery(context);
  }
}
