#include <hash.h>

#define VM_BIN 0
#define VM_FILE 1
#define VM_ANON 2

struct page{
	void *kaddr;
	struct vm_entry *vme;
	struct thread *thread;
	struct list_elem lru_elem;    
};

struct mmap_file{

	int mapid;
	struct file *file;
	struct list_elem elem;
	struct list vme_list;

};



struct vm_entry{
	
	uint8_t type;       // vm_bin, vm_file, vm_anon
	void *vaddr;        // virtual address start
	bool writable; 	    // true -> writable
	bool is_loaded;     // true -> be in physical memory
	bool pinned;        // true -> not victim
	struct file *file;  // mapping file
	struct list_elem mmap_elem;  // mmap list elem
	size_t offset;      // file offset
	size_t read_bytes;   // number of byte readed
	size_t zero_bytes;   // number of zero in memory
	size_t swap_slot;    // swap slot
	struct hash_elem h_elem; // hash table element

};

void vm_init(struct hash *vm);
void vm_destroy(struct hash *vm);
//static unsigned vm_hash_func(const struct hash_elem *e, void *aux);

//static bool vm_less_func(const struct hash_elem *a,
//			const struct hash_elem *b, void *aux);

//static void vm_destroy_func(struct hash_elem *e, void *aux);
struct vm_entry *find_vme(void *vaddr);
bool insert_vme(struct hash *vm, struct vm_entry* vme);
bool delete_vme(struct hash *vm, struct vm_entry* vme); 
bool load_file(void *kaddr, struct vm_entry *vme);
