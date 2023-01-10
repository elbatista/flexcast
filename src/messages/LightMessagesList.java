package messages;

public class LightMessagesList {
    private Item first = null, last = null;
    private int size = 0;
    
    public Item getFirst() {
        return first;
    }

    public Item getLast() {
        return last;
    }

    public int size() {
        return this.size;
    }

    public void add(LightMessage m) {
        Item newItem = new Item(m);
        if(first == null){
            first = last = newItem;
        }
        else {
            newItem.setPrev(last);
            last.setNext(newItem);
            last = newItem;
        }
        size++;
    }

    public ItemHst addHst(LightMessage m) {
        ItemHst newItem = new ItemHst(m);
        if(first == null){
            first = last = newItem;
            newItem.setIdx(0);
        }
        else {
            newItem.setIdx(((ItemHst)last).getIdx()+1);
            newItem.setPrev(last);
            last.setNext(newItem);
            last = newItem;
        }
        size++;
        return newItem;
    }

    public class Item {
        private LightMessage m;
        private Item prev,next;    
        public Item (LightMessage m){
            this.m = m;
            prev = null;
            next = null;
        }
        public LightMessage get(){
            return m;
        }
        public Item getPrev() {
            return prev;
        }
        public void setPrev(Item prev) {
            this.prev = prev;
        }
        public Item getNext() {
            return next;
        }
        public void setNext(Item next) {
            this.next = next;
        }
        @Override
        public boolean equals(Object o){
            return get().equals(((Item) o).get());
        }
        @Override
        public String toString(){
            return String.valueOf(m.getId());
        }
    }

    public class ItemHst extends Item {
        private int idx;
        public ItemHst(LightMessage m) {
            super(m);
        }
        public int getIdx() {
            return idx;
        }
        public void setIdx(int idx) {
            this.idx = idx;
        }
    }
}
