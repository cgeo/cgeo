// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils.offlinetranslate

enum class class TranslatorState {
    UNINITIALIZED, //Not yet initialized (state after creation)
    DETECTING_SOURCE, // detecting/guessing source language from a sample
    MODEL_MISSING, // src and trg are known and valid, but can't tranlate because at least one model is missing
    DOWNLOADING_MODEL, // downloading missing models
    READY, //ready for translate, but no translation requested (translator disabled)
    TRANSLATING, //ready for translate AND currently translating
    TRANSLATED, //ready for translate AND everything is translated
    ERROR //some error occured along the way (eg exception or non-supported src or trg language)
}
