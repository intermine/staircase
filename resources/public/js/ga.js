define(['json!/api/v1/client-config'], function (config) {

  if (!(config && config.ga_token)
      || "localhost" == window.location.hostname) {
    console.debug("Skipping analytics");
    return function () {}; // NO-OP function.
  }

  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

  ga('create', config.ga_token, 'auto');

  return ga;

});
