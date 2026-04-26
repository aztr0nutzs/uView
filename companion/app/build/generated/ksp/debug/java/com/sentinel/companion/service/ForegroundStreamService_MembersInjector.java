package com.sentinel.companion.service;

import com.sentinel.companion.data.repository.DeviceRepository;
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
public final class ForegroundStreamService_MembersInjector implements MembersInjector<ForegroundStreamService> {
  private final Provider<DeviceRepository> repoProvider;

  public ForegroundStreamService_MembersInjector(Provider<DeviceRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  public static MembersInjector<ForegroundStreamService> create(
      Provider<DeviceRepository> repoProvider) {
    return new ForegroundStreamService_MembersInjector(repoProvider);
  }

  @Override
  public void injectMembers(ForegroundStreamService instance) {
    injectRepo(instance, repoProvider.get());
  }

  @InjectedFieldSignature("com.sentinel.companion.service.ForegroundStreamService.repo")
  public static void injectRepo(ForegroundStreamService instance, DeviceRepository repo) {
    instance.repo = repo;
  }
}
