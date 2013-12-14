// ==UserScript==
// @name Send to c:geo for opencaching pl/nl
// @namespace http://send2.cgeo.org/
// @description Add button "Send to c:geo" to opencaching.pl and opencaching.nl
// @include http://opencaching.pl/viewcache.php*
// @include http://www.opencaching.nl/viewcache.php*
// @icon http://send2.cgeo.org/content/images/logo.png
// @version 0.2
// ==/UserScript==

// Inserts javascript that will be called by the s2cgeo button. The closure
// look strange, but avoids having to escape the code.

var s = document.createElement('script');
s.type = 'text/javascript';
s.innerHTML = 'function s2cgeo(code) {\n'
    + '// show the box and the "please wait" text\n'
    + 'document.getElementById("send2cgeo").style.display="block";\n'
    + 'document.getElementById("send2cgeo").childNodes[0].style.display="block";\n'
    + '// hide iframe for now and wait for page to be loaded\n'
    + 'document.getElementById("send2cgeo").childNodes[1]\n'
    + '  .style.display="none";\n'
    + 'document.getElementById("send2cgeo").childNodes[1]\n'
    + '  .onload=function() {\n'
    + '    document.getElementById(\'send2cgeo\').childNodes[0]\n'
    + '       .style.display="none";\n'
    + '    document.getElementById(\'send2cgeo\').childNodes[1]\n'
    + '       .style.display="block";\n'
    + '    // hide box after 3 seconds\n'
	+ '    setTimeout( function() {'
    + '    document.getElementById(\'send2cgeo\').style.display="none";}, 3000);\n'
    + '  };\n'
    + 'document.getElementById("send2cgeo").childNodes[1]\n'
    + '  .src="http://send2.cgeo.org/add.html?cache="+code;\n'
	+ '}';

  // Inject Script.
  document.getElementsByTagName("head")[0].appendChild(s);

  // Defines the elements to insert into the page //////////////////////////////
  var boxWidth = 20,
      boxHeight = 7;

  var boxStyle = 'display:none; background:#1D1D1D; z-index:1000; left:50%;'
               + 'box-shadow:0 0 0.5em #000; padding:0; border:0; '
               + 'position:fixed; top:0.5em; text-align:center; '
               + 'margin-left:-'+(boxWidth/2)+'em; line-height:'+boxHeight+'em;'
               + 'width:'+boxWidth+'em; height:'+boxHeight+'em; color: #fff';
  var waitStyle = 'width: '+boxWidth+'em; color: #fff';
  var iframeStyle = 'border:0; width:'+boxWidth+'em; height: '+boxHeight+'em';

  var b = document.createElement('div');
  b.id = "send2cgeo";
  b.setAttribute("style", boxStyle);
  b.innerHTML = '<div style="'+waitStyle+'">Please wait&hellip;</div>'
    + '<iframe style="'+iframeStyle+'"></iframe>';

  document.getElementsByTagName("body")[0].appendChild(b);
	
  // Append to send2cgeo links/buttons /////////////////////////////////////////
  var oc = document.getElementById('SendToGPS').parentNode.parentNode;

  if(oc !== null) {
    var occode=oc.innerHTML;

    occode = occode.substr(occode.indexOf('wp=')+3,6);

    var html = '></a><p>&nbsp;</p>'
             + '<a class="send-to-gps" '
             + 'href="#" '
             + 'onclick="s2cgeo(\''+occode+'\'); return false;" >'
             + '<input type="button" id="SendToGPS" value="Send to c:geo"'
             + 'name="SendToCgeo" /></a> ';

    oc.innerHTML = oc.innerHTML.replace('></a><p>&nbsp;</p>', html);

  };
