const LoginPage = () => import(/* webpackChunkName: "js/pages/login" */'@/views/auth/LoginPage')
const JoinPage = () => import(/* webpackChunkName: "js/pages/join" */'@/views/auth/JoinPage')
const Mypage = () => import(/* webpackChunkName: "js/pages/mypage" */'@/views/auth/Mypage')
const MypageEdit = () => import(/* webpackChunkName: "js/pages/mypage-edit" */'@/views/auth/MypageEdit')

const authRoutes = [
  {
    path: '/login',
    component: LoginPage
  },
  {
    path: '/join',
    component: JoinPage
  },
  {
    path: '/mypage',
    component: Mypage
  },
  {
    path: '/mypage/edit',
    component: MypageEdit
  }
]
export default authRoutes
