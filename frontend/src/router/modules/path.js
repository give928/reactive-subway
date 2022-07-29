const PathPage = () => import(/* webpackChunkName: "js/pages/path" */'../../views/path/PathPage')

const pathRoutes = [
  {
    path: '/path',
    component: PathPage
  }
]
export default pathRoutes
