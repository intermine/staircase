(ns staircase.views.history
  (:require staircase.views.forms
            staircase.views.facets
            [hiccup.def :refer (defelem)]
            [staircase.views.buttons :as btn])
  (:use [hiccup.core :only (html)]
        [hiccup.element :only (mail-to)]))

(defelem tool-not-found [{contact :contact-email}]
  [:div.alert.alert-danger 
   [:h3 "Error"]
   [:p
    " The required tool for this step could not be found. Check with the site
    administrator to make sure that the "
    [:code "{{step.tool}}"]
    " tool is installed"]
   (update-in (mail-to contact
                       [:span
                        [:i.fa.fa-envelope-o]
                        " Send a grumpy email"])
              [1 :class] (constantly "btn btn-default"))])

(defn snippet [config]
  (html [:div.container-fluid.history-view
   [:div.row

    [:div.sidebar.slide-left.col-xs-12.col-md-2
     {:ng-class "{minimised: state.expanded, collapsed: collapsed}"}
     [:div.current-history.panel.panel-default
      (apply vector
             :div.panel-heading
             (btn/collapse-button)
             (btn/edit-button)
             (btn/cancel-button {:ng-click "controller.updateHistory()"})
             (btn/save-button {:ng-click "controller.saveHistory()"})
             (staircase.views.forms/label-input-pair "history.title"))
      (apply vector
             :div.panel-body
             [:p [:small [:em "{{history.created_at | roughDate }}"]]]
             (staircase.views.forms/label-input-pair "history.description"))
      [:div.list-group.history-steps
        [:a.list-group-item {:ng-repeat "s in steps"
                             :ng-controller "HistoryStepCtrl" 
                             :ng-class "{active: step.id == s.id}"
                             :href "/history/{{history.id}}/{{$index + 1}}"}

         [:i.pull-right.fa.fa-edit
            {:ng-click "openEditDialogue()"
             :ng-show "step.id == s.id"}]
         "{{s.title}}"]]]


     (staircase.views.facets/snippet)]

    [:div.sidebar.slide-right.col-xs-12.col-md-2.col-md-offset-10
     {:ng-class "{onscreen: !state.expanded, offscreen: state.expanded}"}
     [:div.panel.panel-default.next-steps
      [:div.panel-heading
       {:ng-click "state.nextStepsCollapsed = !state.nextStepsCollapsed"}
        [:i.fa.fa-fw {:ng-class "{'fa-caret-right': state.nextStepsCollapsed, 'fa-caret-down': !state.nextStepsCollapsed}"}]
        "Next steps"]
      [:div.list-group.steps.animated
       {:ng-class "{expanded: (nextSteps.length && !state.nextStepsCollapsed)}"}
       [:next-step
        {:ng-repeat "ns in nextSteps"
         :previous-step "step"
         :append-step "controller.nextStep(data)"
         :tool "ns.tool"
         :service "ns.service"
         :data "ns.data"}
        ]]
      [:div.panel-body {:ng-hide "nextSteps.length"}
       [:em "No steps available"]]]]

    [:div.col-xs-12.slide-left
     {:ng-class "{'col-md-8': !state.expanded,
                  'col-md-offset-2': !state.expanded}"}
     (tool-not-found {:ng-show "error.status === 404"} config)
     [:div.current-step
      {:ng-hide "error.status === 404"
       :tool "tool"
       :step "step"
       :state "state"
       :has-items "controller.setItems(key, type, ids)"
       :has "controller.hasSomething(what, data, key)"
       :wants "controller.wantsSomething(what, data)"
       :next-step "controller.nextStep(data)"
       :silently "controller.storeHistory(data)"
       :toggle "state.expanded = !state.expanded"} ]
     ]


    ]]))
