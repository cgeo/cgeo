package cgeo.geocaching.utils.offlinetranslate;

public enum TranslatorState {
    CREATED, //after creation, before initialization
    REINITIALIZED, //after (re)initialization
    SOURCE_LANGUAGE_DETECTED, //source language is determined, model is not (yet) available (but also currently not needed)
    SOURCE_LANGUAGE_DETECTED_MODEL_NEEDED, //source language is determined, model is not yet available but needed to proceed
    READY, //ready for translate, but no translation requested (translator disabled)
    TRANSLATING, //ready for translate AND currently translating
    TRANSLATED, //ready for translate AND everything is translated
    ERROR //some error occured along the way
}
