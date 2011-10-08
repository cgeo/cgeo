// ==UserScript==
// @name           sendToCgeo
// @namespace      http://send2.cgeo.org/
// @description    Add Send to c:geo button to geocaching.com
// @include        http://www.geocaching.com/seek/cache_details*
// @include        http://www.geocaching.com/map/beta*
// @version 	   0.23
// ==/UserScript==

// Inserts javascript that will be called by the s2cgeo button
var s       = document.createElement('script');
s.type      = 'text/javascript';
s.innerHTML =  'function s2cgeo(code) {'
                + 'window.open(\'http://send2.cgeo.org/add.html?cache=\'+code,'
                + '\'cgeo\',\'height=50,width=50\'); }';
document.getElementsByTagName("head")[0].appendChild(s);

var map = document.getElementById('cacheDetailsTemplate');

if( map != null )
{
    var html = 'Log Visit</span></a> <br /> '
             + '<a href="javascript:s2cgeo(\'${cache.gc}\');" '
             + 'class="lnk">'
             + '<img src="/images/sendtogps/sendtogps_icon.png" '
             + 'align="absmiddle" border="0"> '
             + '<span>Send to c:geo</span></a>';
    
    map.innerHTML = map.innerHTML.replace('Log Visit</span></a>', html);
}
else
{
    var d         = document.getElementById('Download');
    var m         = d.children;
    var last      = m.item(m.length-1);
    var GCElement = document.getElementById('ctl00_ContentBody_CoordInfoLinkControl1_uxCoordInfoCode');
    var GCCode    = GCElement.innerHTML;
    
    var html = '| <input type="button" '
             + 'name="ctl00$ContentBody$btnSendTocgeo" '
             + 'value="Send to c:geo" '
             + 'onclick="s2cgeo(\''+GCCode+'\'); '
             + 'return false;" '
             + 'id="ctl00_ContentBody_btnSendTocgeo" />';
    
    last.innerHTML = last.innerHTML + html;
}