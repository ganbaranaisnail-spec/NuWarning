package com.example.nuwarning;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class KeywordAdapter extends RecyclerView.Adapter<KeywordAdapter.KeywordViewHolder> {

    private List<Keyword> keywordList = new ArrayList<>();
    private OnKeywordInteractionListener listener;

    private StateView stateView = null;

    // コンストラクタにリスナーを渡せるように修正
    public KeywordAdapter(OnKeywordInteractionListener listener) {
        this.listener = listener;
    }

    // アイテムの削除後にリストを更新する
    public void setKeywordList(List<Keyword> list) {
        keywordList = list;
        notifyDataSetChanged();  // リストが更新されたことをリサイクラービューに通知
    }

    // 現在のキーワードリストを取得するメソッドを追加
    public List<Keyword> getCurrentList() {
        return keywordList;
    }

    @Override
    public KeywordViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_keyword, parent, false);
        return new KeywordViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(KeywordViewHolder holder, int position) {
        Keyword keyword = keywordList.get(position);
        holder.keywordText.setText(keyword.getName());

        // 現在のチェックボックスの状態を保存
        boolean isChecked = keyword.isChecked();

        // チェックボックスの状態を設定する前にリスナーを一時的に解除
        holder.keywordCheckBox.setOnCheckedChangeListener(null);  // リスナーを一時的に解除
        holder.keywordCheckBox.setChecked(isChecked);  // チェックボックスの状態を設定

        // 編集ボタンの処理
        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditKeyword(position);
            }
        });

        // 削除ボタンの処理
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteKeyword(position);
            }
        });

        // チェックボックスのオン/オフ処理
        holder.keywordCheckBox.setOnCheckedChangeListener((buttonView, isCheckedNew) -> {
            // チェック状態が変更された場合にリスナーを呼び出し、状態を更新
            if (keyword.isChecked() != isCheckedNew) {
//                keyword.setChecked(isCheckedNew); // チェック状態を更新
                if (listener != null) {
                    listener.onToggleKeywordCheck(position);
                }

//                if(keyword.isChecked() && stateView.getState() == StateView.STATE_MONITORING_REQUEST_KEYWORD){
//                    stateView.setState(StateView.STATE_MONITORING);
//                }else if(!keyword.isChecked() && getCheckedKeywords().isEmpty()){
//                    stateView.setState(StateView.STATE_MONITORING_REQUEST_KEYWORD);
//                }
            }
        });
    }

    /**
     * チェックされているキーワードだけをリストで返すメソッド
     */
    public List<Keyword> getCheckedKeywords() {
        List<Keyword> checkedList = new ArrayList<>();
        for (Keyword keyword : keywordList) {
            if (keyword.isChecked()) {
                checkedList.add(keyword);
            }
        }
        return checkedList;
    }

    @Override
    public int getItemCount() {
        return keywordList.size();
    }

    public void setStateView(StateView stateViewInstance){
        stateView = stateViewInstance;
    }

    // KeywordViewHolderクラスの定義
    public class KeywordViewHolder extends RecyclerView.ViewHolder {
        TextView keywordText;
        ImageButton editButton, deleteButton;
        CheckBox keywordCheckBox;

        public KeywordViewHolder(View itemView) {
            super(itemView);
            keywordText = itemView.findViewById(R.id.keywordText);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            keywordCheckBox = itemView.findViewById(R.id.checkbox);
        }
    }

    // リスナーインターフェース
    public interface OnKeywordInteractionListener {
        void onEditKeyword(int position);
        void onDeleteKeyword(int position);
        void onToggleKeywordCheck(int position);
    }
}
