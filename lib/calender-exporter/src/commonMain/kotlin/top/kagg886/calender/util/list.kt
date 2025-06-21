package top.kagg886.calender.util

sealed class ListChange<T> {
    data class Added<T>(val item: T, val index: Int) : ListChange<T>()
    data class Removed<T>(val item: T, val index: Int) : ListChange<T>()
    data class Updated<T>(val oldItem: T, val newItem: T, val index: Int) : ListChange<T>()
}

class ObservableMutableList<T>(
    private val delegate: MutableList<T>,
) : MutableList<T> by delegate {

    private val observers = mutableListOf<(ListChange<T>) -> Unit>()

    fun addObserver(observer: (ListChange<T>) -> Unit) {
        observers += observer
    }

    fun removeObserver(observer: (ListChange<T>) -> Unit) {
        observers -= observer
    }

    private fun notify(change: ListChange<T>) {
        observers.forEach { it(change) }
    }

    override fun add(element: T): Boolean {
        val result = delegate.add(element)
        if (result) notify(ListChange.Added(element, delegate.lastIndex))
        return result
    }

    override fun add(index: Int, element: T) {
        delegate.add(index, element)
        notify(ListChange.Added(element, index))
    }

    override fun removeAt(index: Int): T {
        val removed = delegate.removeAt(index)
        notify(ListChange.Removed(removed, index))
        return removed
    }

    override fun set(index: Int, element: T): T {
        val old = delegate[index]
        delegate[index] = element
        notify(ListChange.Updated(old, element, index))
        return old
    }
}
