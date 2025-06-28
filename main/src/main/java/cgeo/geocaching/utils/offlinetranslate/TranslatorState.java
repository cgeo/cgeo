package cgeo.geocaching.utils.offlinetranslate;

public enum TranslatorState {
    CREATED, //after creation, before initialization
    REINITIALIZED, //after (re)initialization -> should trigger restoring original text
    SOURCE_LANGUAGE_DETECTED, //after source language is determined (->in this state model download may happening)
    READY, //ready for translate, but no translation done (eg because disabled or no action registered)
    TRANSLATING, //ready for translate AND currently translating
    TRANSLATED, //ready for translate AND registered actions are translated
    ERROR //some error occured along the way
}
