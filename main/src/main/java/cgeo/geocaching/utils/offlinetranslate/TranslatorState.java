package cgeo.geocaching.utils.offlinetranslate;

public enum TranslatorState {
    UNINITIALIZED, //Not yet initialized (state after creation)
    DETECTING_SOURCE, // detecting/guessing source language from a sample
    MODEL_MISSING, // src and trg are known and valid, but can't tranlate because at least one model is missing
    DOWNLOADING_MODEL, // downloading missing models
    READY, //ready for translate, but no translation requested (translator disabled)
    TRANSLATING, //ready for translate AND currently translating
    TRANSLATED, //ready for translate AND everything is translated
    ERROR //some error occured along the way (eg exception or non-supported src or trg language)
}
