deps = ['require', 'angular', 'app']

define deps, (require, ng) ->
  # The call to setTimeout is here as it makes loading the app considerably more reliable.
  # Depending on compilation sequence, various modules were not being found. This is dumb, and
  # a better way ought to be found.
  require ['domReady!'], (document) ->
  	setTimeout (-> ng.bootstrap document, ['steps']), 50

