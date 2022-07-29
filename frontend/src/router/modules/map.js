const MapPage = () => import(/* webpackChunkName: "js/pages/map" */'../../views/map/MapPage')

const mapRoutes = [
  {
    path: '/maps',
    component: MapPage
  }
]
export default mapRoutes
