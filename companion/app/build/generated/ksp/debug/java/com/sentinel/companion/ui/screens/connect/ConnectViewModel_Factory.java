package com.sentinel.companion.ui.screens.connect;

import com.sentinel.companion.data.network.HostValidator;
import com.sentinel.companion.data.repository.PreferencesRepository;
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
public final class ConnectViewModel_Factory implements Factory<ConnectViewModel> {
  private final Provider<PreferencesRepository> prefsRepoProvider;

  private final Provider<HostValidator> hostValidatorProvider;

  public ConnectViewModel_Factory(Provider<PreferencesRepository> prefsRepoProvider,
      Provider<HostValidator> hostValidatorProvider) {
    this.prefsRepoProvider = prefsRepoProvider;
    this.hostValidatorProvider = hostValidatorProvider;
  }

  @Override
  public ConnectViewModel get() {
    return newInstance(prefsRepoProvider.get(), hostValidatorProvider.get());
  }

  public static ConnectViewModel_Factory create(Provider<PreferencesRepository> prefsRepoProvider,
      Provider<HostValidator> hostValidatorProvider) {
    return new ConnectViewModel_Factory(prefsRepoProvider, hostValidatorProvider);
  }

  public static ConnectViewModel newInstance(PreferencesRepository prefsRepo,
      HostValidator hostValidator) {
    return new ConnectViewModel(prefsRepo, hostValidator);
  }
}
