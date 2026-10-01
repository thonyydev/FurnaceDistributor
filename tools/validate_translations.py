"""Validate all source or packaged translations using only the Python standard library."""
import json
import re
from pathlib import Path


LANGUAGES = ("en_us", "pt_br", "es_es", "fr_fr", "de_de", "it_it", "pl_pl", "ru_ru",
             "zh_cn", "zh_tw", "ja_jp", "ko_kr")
FORMAT_TOKENS = re.compile(r"%(?:[0-9]+\$)?[-#+ 0,(<]*[0-9]*(?:\.[0-9]+)?[a-zA-Z%]|§[0-9a-fk-or]", re.I)
DIRECTORY = Path(__file__).resolve().parents[1] / "common/src/main/resources/assets/furnacedistributor/lang"


def unique_keys(pairs):
    result = {}
    for key, value in pairs:
        assert key not in result, f"Duplicate translation key: {key}"
        result[key] = value
    return result


def validate_languages(resources):
    assert set(LANGUAGES) <= resources.keys(), f"Missing languages: {set(LANGUAGES) - resources.keys()}"
    documents = {locale: json.loads(data.decode("utf-8"), object_pairs_hook=unique_keys)
                 for locale, data in resources.items()}
    reference = documents["en_us"]
    for locale, translated in documents.items():
        assert isinstance(translated, dict), f"{locale}: expected a JSON object"
        assert translated.keys() == reference.keys(), (
            f"{locale}: missing {reference.keys() - translated.keys()}, extra {translated.keys() - reference.keys()}"
        )
        for key, value in translated.items():
            label = f"{locale}: {key}"
            assert isinstance(value, str) and value.strip(), f"{label}: empty or non-string value"
            assert "\ufffd" not in value, f"{label}: invalid Unicode replacement character"
            original = reference[key]
            assert FORMAT_TOKENS.findall(value) == FORMAT_TOKENS.findall(original), f"{label}: changed format tokens"
            assert value.count("Furnace Distributor") == original.count("Furnace Distributor"), f"{label}: changed mod name"
            for symbol in ("✓", "·", "+"):
                assert value.count(symbol) == original.count(symbol), f"{label}: changed formatting symbol {symbol}"
        assert translated["key.categories.furnacedistributor"] == "Furnace Distributor", f"{locale}: changed category name"
    return documents


if __name__ == "__main__":
    documents = validate_languages({path.stem: path.read_bytes() for path in sorted(DIRECTORY.glob("*.json"))})
    for locale, values in documents.items():
        print(f"Validated {locale}: {len(values)} keys, valid UTF-8 JSON, placeholders, formatting and mod name")
