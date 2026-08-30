package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.integration.masa.MasaModAvailability;

import java.util.List;

final class MasaGuideCatalog {
    private MasaGuideCatalog() {}

    static List<Entry> entries(MasaModAvailability.Snapshot installed) {
        return List.of(
                new Entry(
                        "MaLiLib",
                        installed.malilib(),
                        "Masa系MODの共通GUI・Hotkey・設定基盤です。",
                        "主な場所: Mod Menu → MaLiLib → Configs。共通のGeneric / Hotkeys / Lists操作を担当します。"),
                new Entry(
                        "Litematica",
                        installed.litematica(),
                        "設計図の表示・配置確認・材料リスト・Verifierを担当します。",
                        "主な場所: Litematicaの設定画面 → Generic / Visuals / Hotkeys。設計図管理はSchematic、配置管理はPlacements、検証はVerifierを確認します。"),
                new Entry(
                        "Tweakeroo",
                        installed.tweakeroo(),
                        "入力・配置・視点・Tool Switchなど多数のTweakを提供します。",
                        "主な場所: Tweakerooの設定画面 → Generic / Tweaks / Hotkeys / Lists。ChiseのGuardは既存Tweakの実行可否だけを制御します。"),
                new Entry(
                        "TweakerMore",
                        installed.tweakermore(),
                        "Tweakeroo/Litematicaを拡張する追加機能群です。",
                        "主な場所: TweakerMoreの設定画面。Schematic Pro PlaceやAuto Collect Material List Itemなど、元機能側の設定を先に確認します。"),
                new Entry(
                        "Syncmatica",
                        installed.syncmatica(),
                        "Litematicaの設計図配置をサーバー参加者と共有します。",
                        "主な場所: SyncmaticaのServer Placements画面。ChiseはRemoveの禁止/Shift必須Guardだけを追加し、通信自体はSyncmaticaが所有します。"));
    }

    record Entry(String name, boolean installed, String summary, String directions) {}
}
