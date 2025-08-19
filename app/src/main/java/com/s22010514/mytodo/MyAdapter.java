//package com.s22010514.mytodo;
//
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//
//public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHoder> {
//
//    context context;
//
//    //create constructor
//    public MyAdapter(Context context, NoteList noteList) {
//        this.context = context;
//        this.n
//
//    }
//
//    @NonNull
//    @Override
//    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
//        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.note_item,parent,false));
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull MyAdapter.MyViewHolder holder, int viewType) {
//        Note note = noteList.get(position);
//
//    }
//
//
//    @Override
//    public int getItemCount(){
//        return 0;
//    }
//
//    public class MyViewHolder extends RecyclerView.ViewHolder{
//
//        TextView titleOutput;
//        TextView bodyOutput;
//        TextView timeOutput;
//
//
//        public MyViewHolder(@nonNull View itemView){
//            super(itemView);
//            titleOutput = itemView.findViewById(R.id.titleOutput);
//            bodyOutput = itemView.findViewById(R.id.bodyOutput);
//            timeOutput = itemView.findViewById(R.id.timeOutput);
//
//        }
//    }
//}
