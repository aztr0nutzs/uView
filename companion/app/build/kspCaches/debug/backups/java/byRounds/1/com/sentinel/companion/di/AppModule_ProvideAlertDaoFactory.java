package com.sentinel.companion.di;

import com.sentinel.companion.data.db.AlertDao;
import com.sentinel.companion.data.db.AppDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideAlertDaoFactory implements Factory<AlertDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideAlertDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public AlertDao get() {
    return provideAlertDao(dbProvider.get());
  }

  public static AppModule_ProvideAlertDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideAlertDaoFactory(dbProvider);
  }

  public static AlertDao provideAlertDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAlertDao(db));
  }
}
