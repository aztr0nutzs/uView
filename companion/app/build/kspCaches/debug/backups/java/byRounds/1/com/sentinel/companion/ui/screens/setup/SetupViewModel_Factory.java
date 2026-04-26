package com.sentinel.companion.ui.screens.setup;

import com.sentinel.companion.data.network.NetworkDiscovery;
import com.sentinel.companion.data.repository.DeviceRepository;
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
public final class SetupViewModel_Factory implements Factory<SetupViewModel> {
  private final Provider<NetworkDiscovery> discoveryProvider;

  private final Provider<DeviceRepository> repoProvider;

  public SetupViewModel_Factory(Provider<NetworkDiscovery> discoveryProvider,
      Provider<DeviceRepository> repoProvider) {
    this.discoveryProvider = discoveryProvider;
    this.repoProvider = repoProvider;
  }

  @Override
  public SetupViewModel get() {
    return newInstance(discoveryProvider.get(), repoProvider.get());
  }

  public static SetupViewModel_Factory create(Provider<NetworkDiscovery> discoveryProvider,
      Provider<DeviceRepository> repoProvider) {
    return new SetupViewModel_Factory(discoveryProvider, repoProvider);
  }

  public static SetupViewModel newInstance(NetworkDiscovery discovery, DeviceRepository repo) {
    return new SetupViewModel(discovery, repo);
  }
}
