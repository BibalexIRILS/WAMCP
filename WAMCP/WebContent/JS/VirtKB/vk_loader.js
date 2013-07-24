/**
 *  $Id: vk_loader.js 666 2009-09-22 14:03:40Z wingedfox $
 *
 *  Keyboard loader
 *
 *  This software is protected by patent No.2009611147 issued on 20.02.2009 by Russian Federal Service for Intellectual Property Patents and Trademarks.
 *
 *  @author Ilya Lebedev
 *  @copyright 2006-2009 Ilya Lebedev <ilya@lebedev.net>
 *  @version $Rev: 666 $
 *  @lastchange $Author: wingedfox $ $Date: 2009-09-22 18:03:40 +0400 (Втр, 22 Сен 2009) $
 */
VirtualKeyboard = new function () {
  var self = this, to = null;
  self.show = self.hide = self.toggle = self.attachInput = function () {
     window.status = 'VirtualKeyboard is not loaded yet.';
     if (!to) setTimeout(function(){window.status = ''},1000);
  }
  self.isOpen = function () {
      return false;
  }
};
(function () {
    var p = (function (sname){var sc=document.getElementsByTagName('script'),sr=new RegExp('^(.*/|)('+sname+')([#?]|$)');for (var i=0,scL=sc.length; i<scL; i++) {var m = String(sc[i].src).match(sr);if (m) {if (m[1].match(/^((https?|file)\:\/{2,}|\w:[\\])/)) return m[1];if (m[1].indexOf("/")==0) return m[1];b = document.getElementsByTagName('base');if (b[0] && b[0].href) return b[0].href+m[1];return (document.location.href.match(/(.*[\/\\])/)[0]+m[1]).replace(/^\/+/,"");}}return null;})
             ('vk_loader.js');

    var dpd = [ 'extensions/helpers.js'
               ,'extensions/dom.js'
               ,'extensions/ext/object.js'
               ,'extensions/ext/string.js'
               ,'extensions/ext/regexp.js'
               ,'extensions/ext/array.js'
               ,'extensions/eventmanager.js'
               ,'extensions/documentselection.js'
               ,'extensions/documentcookie.js'
/*
* not used by default
*
*               ,'layouts/unconverted.js'
*/
    ];

    for (var i=0,dL=dpd.length;i<dL;i++)
        dpd[i] = p+dpd[i];
    dpd[i++] = p+'virtualkeyboard.js';
    dpd[i] = p+'layouts/layouts.js';
    if (window.ScriptQueue) {
        ScriptQueue.queue(dpd);
    } else {
        if (!(window.ScriptQueueIncludes instanceof Array)) window.ScriptQueueIncludes = []
        window.ScriptQueueIncludes = window.ScriptQueueIncludes.concat(dpd);

        /*
        *  attach script loader
        */
        if (document.body) {
            s = document.createElement('script');
            s.type="text/javascript";
            s.src = p+'extensions/scriptqueue.js';
            head.appendChild(s);
        } else {
            document.write("<scr"+"ipt type=\"text/javascript\" src=\""+p+'extensions/scriptqueue.js'+"\"></scr"+"ipt>");
        }
    }
})();
