const LinePage = () => import(/* webpackChunkName: "js/pages/line" */'@/views/line/LinePage')

const lineRoutes = [
  {
    path: '/lines',
    component: LinePage
  }
]
export default lineRoutes
