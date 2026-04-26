package com.sentinel.companion.ui.screens.settings;

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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<PreferencesRepository> prefsRepoProvider;

  public SettingsViewModel_Factory(Provider<PreferencesRepository> prefsRepoProvider) {
    this.prefsRepoProvider = prefsRepoProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(prefsRepoProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<PreferencesRepository> prefsRepoProvider) {
    return new SettingsViewModel_Factory(prefsRepoProvider);
  }

  public static SettingsViewModel newInstance(PreferencesRepository prefsRepo) {
    return new SettingsViewModel(prefsRepo);
  }
}
