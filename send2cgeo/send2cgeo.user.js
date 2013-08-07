// ==UserScript==
// @name           Send to c:geo
// @namespace      http://send2.cgeo.org/
// @description    Add button "Send to c:geo" to geocaching.com
// @include        http://www.geocaching.com/seek/cache_details*
// @include        http://www.geocaching.com/map/*
// @include        http://www.geocaching.com/geocache/*
// @include        http://www.geocaching.com/my/recentlyviewedcaches*
// @icon           http://send2.cgeo.org/content/images/logo.png
// @updateURL      http://send2.cgeo.org/send2cgeo.user.js
// @version        0.27
// ==/UserScript==

// Inserts javascript that will be called by the s2cgeo button. The closure
// look strange, but avoids having to escape the code. Almost everything
// is put into that script element so that geocaching.com's jQuery may be
// accessed.

var s       = document.createElement('script');
s.type      = 'text/javascript';
s.textContent =  '(' + function() {
  // function that handles the actual sending //////////////////////////////////

  window.s2geo = function(code) {
    // show the box and the "please wait" text
    $('#send2cgeo, #send2cgeo div').fadeIn();
    // hide iframe for now and wait for page to be loaded
    $('#send2cgeo iframe')
      .hide()
      .unbind('load')
      .attr('src', 'http://send2.cgeo.org/add.html?cache='+code)
      .load(function() {
        // hide "please wait text" and show iframe
        $('#send2cgeo div').hide();
        // hide box after 3 seconds
        $(this).show().parent().delay(3000).fadeOut();
      });
  };


  // Defines the elements to insert into the page //////////////////////////////
  var boxWidth = 20,
      boxHeight = 7;

  var boxStyle = 'display:none; background:#1D1D1D; z-index:1000; left:50%;'
               + 'box-shadow:0 0 0.5em #000; padding:0; border:0; '
               + 'position:fixed; top:0.5em;  text-align:center; '
               + 'margin-left:-'+(boxWidth/2)+'em; line-height:'+boxHeight+'em;'
               + 'width:'+boxWidth+'em; height:'+boxHeight+'em; color: #fff';
  var waitStyle = 'width: '+boxWidth+'em; color: #fff';
  var iframeStyle = 'border:0; width:'+boxWidth+'em; height: '+boxHeight+'em';

  $('body').append('<div id="send2cgeo" style="'+boxStyle+'">'
    + '<div style="'+waitStyle+'">Please wait&hellip;</div>'
    + '<iframe style="'+iframeStyle+'"></iframe>'
    + '</div>');


  // Append to send2cgeo links/buttons /////////////////////////////////////////
  var map = document.getElementById('cacheDetailsTemplate');

  if(map !== null) {
    // geocaching.com map view
    var html = 'Log Visit</span></a> <br /> '
             + '<a class="lnk ui-block-b" '
             + 'href="http://send2.cgeo.org/add.html?cache={{=gc}}" '
             + 'onclick="window.s2geo(\'{{=gc}}\'); return false;" '
             + 'class="lnk">'
             + '<img src="/images/sendtogps/sendtogps_icon.png" '
             + 'align="absmiddle" border="0"> '
             + '<span>Send to c:geo</span>';

    map.innerHTML = map.innerHTML.replace('Log Visit</span>', html);
  } else if(document.getElementById('ctl00_ContentBody_CoordInfoLinkControl1_uxCoordInfoCode') != null){
    // geocaching.com cache detail page
    var GCCode = $('#ctl00_ContentBody_CoordInfoLinkControl1_uxCoordInfoCode')
                  .html();

    var html = ' | <input type="button" '
             + 'value="Send to c:geo" '
             + 'onclick="window.s2geo(\''+GCCode+'\'); '
             + 'return false;" '
             + '/>';

    $('#Download p:last').append(html);
    $('#Download dd:last').append(html);
  } else {
    // geocaching.com recentlyviewed
    $('img[src="/images/icons/16/send_to_gps.png"]').each(function(){
      $(this).attr('alt', "Send to c:geo").attr('title', "Send to c:geo");
    });
    $('a[title="Send to GPS"]').each(function(){
      var text = $(this).parent().parent().find(".Merge").last().find(".small").first().text().split("|");
      var GCCode = text[text.length - 2].trim();
      this.href="javascript:window.s2geo('"+GCCode+"')";
      this.title = "Send to c:geo";
    });
    
  }
} + ')();';

// Inject Script. Can’t use jQuery yet, because the page is not
// accessible from Tampermonkey
document.getElementsByTagName("head")[0].appendChild(s);
