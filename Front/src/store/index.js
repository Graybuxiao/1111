import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)


export default new Vuex.Store({
  state: {
    nodeMap: new Map(),
    isEdit: null,
    selectedNode: {},
    selectedParentNode:{},
    selectFormItem: null,
    design:{},
    runningList: [],
    noTakeList: [],
    endList: [],
    diagramMode: 'design',
  },
  mutations: {
    selectedParentNode(state, val) {
      state.selectedParentNode = val
    },
    selectedNode(state, val) {
      state.selectedNode = val
    },
    loadForm(state, val){
      state.design = val
    },
    setIsEdit(state, val){
      state.isEdit = val
    }
  },
  getters: {},
  actions: {},
  modules: {}
})
