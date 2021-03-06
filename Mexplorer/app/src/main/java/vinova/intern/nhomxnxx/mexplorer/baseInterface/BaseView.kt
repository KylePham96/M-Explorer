package vinova.intern.nhomxnxx.mexplorer.baseInterface

interface BaseView<T> {
    fun setPresenter(presenter: T)

    fun showLoading(isShow: Boolean)

    fun showError(message: String)

    fun updateUser()

    fun forceLogOut(message : String)

}
