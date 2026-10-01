"""Validate the installable jars after Gradle build; uses Python standard library only."""
import json
import re
import struct
import tomllib
import zipfile
from pathlib import Path
from validate_translations import DIRECTORY, validate_languages


ROOT = Path(__file__).resolve().parents[1]
PROPERTIES = dict(re.findall(r"^([\w_]+)\s*=\s*(.*?)\s*$", (ROOT / "gradle.properties").read_text(), re.M))
VERSION = PROPERTIES["mod_version"]
PREFIX = "com/thonyy/furnacedistributor/"


def validate(loader):
    path = ROOT / loader / "build/libs" / f"furnacedistributor-{loader}-{VERSION}.jar"
    with zipfile.ZipFile(path) as jar:
        names = jar.namelist()
        assert len(names) == len(set(names)), f"Duplicate entries in {path.name}"
        for name in ["FurnaceDistributor", "logic/OperationGuard", "logic/FurnaceArea", "logic/DistributionPlan",
                     "config/ServerConfig", "client/ClientConfig", "client/SelectionHud", "client/CollectionShortcut", "client/DistributorSettingsScreen",
                     "network/DistributePacket", "network/CollectPacket"]:
            assert PREFIX + name + ".class" in names, f"Missing shared class: {name}"
        assert not any("RegressionChecks" in name for name in names), "Regression code shipped in production"
        for name in names:
            if name.startswith(PREFIX) and name.endswith(".class"):
                assert struct.unpack(">H", jar.read(name)[6:8])[0] == 65, f"Wrong Java version: {name}"
        language_paths = sorted(DIRECTORY.glob("*.json"))
        resources = {file.stem: jar.read(f"assets/furnacedistributor/lang/{file.name}") for file in language_paths}
        validate_languages(resources)
        for file in language_paths:
            assert resources[file.stem] == file.read_bytes(), f"Stale packaged translation: {file.name}"
        mixins = json.loads(jar.read("furnacedistributor.mixins.json"))
        assert mixins["compatibilityLevel"] == "JAVA_21"
        assert not mixins["client"] and not mixins["mixins"], "Unexpected mixin"
        if loader == "fabric":
            metadata = json.loads(jar.read("fabric.mod.json"))
            assert metadata["id"] == "furnacedistributor" and metadata["version"] == VERSION
            assert metadata["depends"]["minecraft"] == PROPERTIES["minecraft_version"]
            assert metadata["entrypoints"]["modmenu"] == [
                "com.thonyy.furnacedistributor.fabric.client.FurnaceDistributorModMenu"
            ], "Missing Mod Menu integration"
            assert "modmenu" not in metadata["depends"], "Mod Menu must remain optional"
            assert not any(name.startswith("com/terraformersmc/") for name in names), "Mod Menu was bundled"
            for entries in metadata["entrypoints"].values():
                for entry in entries:
                    assert entry.replace(".", "/") + ".class" in names, f"Missing entrypoint {entry}"
            if "icon" in metadata:
                assert metadata["icon"] in names, "Missing icon"
        else:
            assert PREFIX + "fabric/client/FurnaceDistributorModMenu.class" not in names, "Fabric integration shipped in NeoForge"
            metadata = tomllib.loads(jar.read("META-INF/neoforge.mods.toml").decode())
            assert metadata["mods"][0]["modId"] == "furnacedistributor"
            assert metadata["mods"][0]["version"] == VERSION
            assert metadata["mixins"][0]["config"] in names
            assert json.loads(jar.read("pack.mcmeta"))["pack"]["pack_format"] == 34
        assert "${" not in json.dumps(metadata), "Unexpanded Gradle placeholder"
    print(f"Validated {path.name}: shared classes, Java 21, languages, mixins and loader metadata")


if __name__ == "__main__":
    for platform in ("fabric", "neoforge"):
        validate(platform)
