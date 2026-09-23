package ir.cyclops.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TubeAdapter : RecyclerView.Adapter<TubeAdapter.VH>() {
    private var items: List<Tube> = emptyList()
    private var currentIdx: Int = 0

    fun submit(list: List<Tube>, idx: Int) {
        items = list; currentIdx = idx; notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tube, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = items[position]
        holder.name.text = t.id
        holder.temp.text = t.temperature?.let { "%.1f°".format(it) } ?: "—"
        holder.idx.text = (position + 1).toString()

        when {
            position < currentIdx -> {
                holder.itemView.setBackgroundColor(Color.parseColor("#1B5E20"))
                setTextColors(holder, Color.WHITE)
            }
            position == currentIdx -> {
                holder.itemView.setBackgroundColor(Color.parseColor("#FF6F00"))
                setTextColors(holder, Color.WHITE)
            }
            else -> {
                holder.itemView.setBackgroundColor(Color.parseColor("#212121"))
                setTextColors(holder, Color.parseColor("#9E9E9E"))
            }
        }
    }

    private fun setTextColors(h: VH, c: Int) {
        h.name.setTextColor(c); h.temp.setTextColor(c); h.idx.setTextColor(c)
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tubeName)
        val temp: TextView = v.findViewById(R.id.tubeTemp)
        val idx: TextView = v.findViewById(R.id.tubeIdx)
    }
}