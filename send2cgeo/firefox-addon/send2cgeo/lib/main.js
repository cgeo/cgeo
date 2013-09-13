var pageMod = require("sdk/page-mod");
var data = require("sdk/self").data;
 
pageMod.PageMod({
  include: ["http://www.geocaching.com/seek/cache_details*", "http://www.geocaching.com/geocache/*", "http://www.geocaching.com/map/*"],
  contentScriptWhen: "end",
  contentScriptFile: data.url("send2cgeo.user.js")
});