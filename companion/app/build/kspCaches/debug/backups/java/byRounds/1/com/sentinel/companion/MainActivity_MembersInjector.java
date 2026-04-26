package com.sentinel.companion;

import com.sentinel.companion.data.repository.PreferencesRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<PreferencesRepository> prefsRepoProvider;

  public MainActivity_MembersInjector(Provider<PreferencesRepository> prefsRepoProvider) {
    this.prefsRepoProvider = prefsRepoProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<PreferencesRepository> prefsRepoProvider) {
    return new MainActivity_MembersInjector(prefsRepoProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectPrefsRepo(instance, prefsRepoProvider.get());
  }

  @InjectedFieldSignature("com.sentinel.companion.MainActivity.prefsRepo")
  public static void injectPrefsRepo(MainActivity instance, PreferencesRepository prefsRepo) {
    instance.prefsRepo = prefsRepo;
  }
}
