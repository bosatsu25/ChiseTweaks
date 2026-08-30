package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.ChiseTweaksClient;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Apply管理下の設定保存をdomain単位で独立実行し、失敗domainだけを呼び出し側へ返す。 */
public final class SettingPersistenceCoordinator {
    private final BooleanSupplier featureSaver;
    private final BooleanSupplier localSaver;
    private final BooleanSupplier integrationSaver;

    SettingPersistenceCoordinator(
            BooleanSupplier featureSaver,
            BooleanSupplier localSaver) {
        this(featureSaver, localSaver, () -> true);
    }

    SettingPersistenceCoordinator(
            BooleanSupplier featureSaver,
            BooleanSupplier localSaver,
            BooleanSupplier integrationSaver) {
        this.featureSaver = featureSaver;
        this.localSaver = localSaver;
        this.integrationSaver = integrationSaver;
    }

    public static SettingPersistenceCoordinator production() {
        return new SettingPersistenceCoordinator(
                FeatureConfig::saveToFile,
                () -> LocalFeatureConfig.getInstance().save(),
                () -> MasaIntegrationConfig.getInstance().save());
    }

    public SaveResult save(Set<SettingPersistence> requestedDomains) {
        EnumSet<SettingPersistence> requested = applyManagedDomains(requestedDomains);
        EnumSet<SettingPersistence> failed = EnumSet.noneOf(SettingPersistence.class);
        for (SettingPersistence domain : requested) {
            if (!saveDomain(domain)) failed.add(domain);
        }
        return new SaveResult(failed);
    }

    private boolean saveDomain(SettingPersistence domain) {
        BooleanSupplier saver = domain == SettingPersistence.FEATURE_CONFIG
                ? featureSaver
                : domain == SettingPersistence.LOCAL_CONFIG
                        ? localSaver
                        : domain == SettingPersistence.INTEGRATION_CONFIG
                                ? integrationSaver
                                : null;
        if (saver == null) return true;
        try {
            return saver.getAsBoolean();
        } catch (RuntimeException | LinkageError failure) {
            ChiseTweaksClient.LOGGER.warn(
                    "Settings persistence domain {} failed after {}",
                    domain.name().toLowerCase(),
                    failure.getClass().getSimpleName());
            return false;
        }
    }

    private static EnumSet<SettingPersistence> applyManagedDomains(Set<SettingPersistence> requestedDomains) {
        EnumSet<SettingPersistence> result = EnumSet.noneOf(SettingPersistence.class);
        if (requestedDomains == null) return result;
        for (SettingPersistence domain : requestedDomains) {
            if (domain != null && domain.isApplyManaged()) result.add(domain);
        }
        return result;
    }

    public record SaveResult(Set<SettingPersistence> failedDomains) {
        public SaveResult {
            failedDomains = failedDomains == null || failedDomains.isEmpty()
                    ? Set.of()
                    : Set.copyOf(failedDomains);
        }

        public boolean successful() {
            return failedDomains.isEmpty();
        }
    }
}
